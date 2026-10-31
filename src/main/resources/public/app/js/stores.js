/*global angular */

(function (angular) {
    'use strict';

    var POLL_MS = 60000;

    /**
     * One piece of server data shared by every component that shows it.
     * Components read `data` and keep it current with use(); writes call
     * invalidate(), which refetches now if the data is shown and on the next
     * use() otherwise, set() with data the write's response carried, or
     * update() to apply the write to the held data.
     *
     * Every request that can change a resource's data takes a ticket from
     * Resource.ticket() when it is sent, and a resource keeps only data from
     * a ticket at least as new as the one it holds, so a poll or response
     * that was already in flight cannot overwrite newer data. options.merge
     * can keep parts of the held data that are newer than a fetch.
     */
    function createResourceFactory($q, Poller) {
        var tickets = 0;

        function Resource(fetch, options) {
            options = options || {};
            var self = this;
            var pollMs = options.pollMs || 0;
            var fetchedAt = null;
            var stale = false;
            var held = 0;
            var latestFetch = 0;
            var pending = null;
            var users = 0;
            var stopPolling = null;

            this.data = null;

            if (options.initial !== null && options.initial !== undefined) {
                self.data = options.initial;
                fetchedAt = Date.now();
            }

            function accept(ticket, data) {
                if (ticket < held) {
                    return;
                }
                held = ticket;
                self.data = data;
                fetchedAt = Date.now();
                stale = false;
            }

            this.refresh = function () {
                var request = Resource.ticket();
                latestFetch = request;
                pending = fetch().then(function (data) {
                    accept(request, options.merge ? options.merge(data, request) : data);
                    return self.data;
                }).finally(function () {
                    if (request === latestFetch) {
                        pending = null;
                    }
                });
                return pending;
            };

            // Data from a write's response; `ticket` is the one the write took when it was sent.
            this.set = function (data, ticket) {
                accept(ticket, data);
            };

            // Replaces the held data with change(data); `ticket` is the one the write took when it was sent.
            this.update = function (change, ticket) {
                if (self.data === null) {
                    self.invalidate();
                    return;
                }
                self.data = change(self.data);
                held = Math.max(held, ticket);
            };

            this.heldTicket = function () {
                return held;
            };

            function isCurrent() {
                return fetchedAt !== null && !stale;
            }

            function isPollDue() {
                return pollMs && fetchedAt !== null && Date.now() - fetchedAt >= pollMs;
            }

            // Resolves with the data, fetching it only when what is held is
            // missing or invalidated, whatever its age.
            this.load = function () {
                if (pending) {
                    return pending;
                }
                return isCurrent() ? $q.resolve(self.data) : self.refresh();
            };

            // Keeps the data current until the returned function is called.
            this.use = function () {
                var released = false;
                users++;
                if (!pending && isPollDue()) {
                    self.refresh();
                } else {
                    self.load();
                }
                if (users === 1 && pollMs) {
                    stopPolling = Poller.start(self.refresh, pollMs);
                }
                return function release() {
                    if (released) {
                        return;
                    }
                    released = true;
                    users--;
                    if (users === 0 && stopPolling) {
                        stopPolling();
                        stopPolling = null;
                    }
                };
            };

            this.invalidate = function () {
                if (users > 0) {
                    self.refresh();
                } else {
                    stale = true;
                }
            };
        }

        Resource.ticket = function () {
            return ++tickets;
        };

        return Resource;
    }


    var PARTIES_PER_PAGE = 20;

    /**
     * The user's newest parties. One more than is shown is fetched, so that
     * hasMore() knows whether showMore() would reveal any; DefaultController
     * embeds the first page the same way.
     */
    function PartiesStore(Resource, RyyppyAPI, InitialData) {
        var self = this;
        var shown = PARTIES_PER_PAGE;

        this.parties = new Resource(function () {
            return RyyppyAPI.getParties(shown + 1);
        }, {initial: InitialData.get('PARTIES')});

        this.shown = function () {
            return shown;
        };

        this.hasMore = function () {
            return self.parties.data !== null && self.parties.data.length > shown;
        };

        this.showMore = function () {
            shown += PARTIES_PER_PAGE;
            return self.parties.refresh();
        };

        this.addParty = function (name, days) {
            return RyyppyAPI.addParty(name, days).then(function (party) {
                self.parties.invalidate();
                return party;
            });
        };

        this.joinWithLink = function (token) {
            return RyyppyAPI.joinWithLink(token).then(function (party) {
                self.parties.invalidate();
                return party;
            });
        };
    }


    /**
     * A party, its participants, the users that can be invited to it and its
     * join link, per party id. Only party members may read these
     * (docs/access-rules.md), so a party's entry is dropped once the user
     * leaves or is removed from it.
     */
    function PartyStore($q, Resource, RyyppyAPI, PartiesStore) {
        var self = this;
        var entries = {};

        function createEntry(key) {
            var entry = {};

            function forgetOnForbidden(fetch) {
                return function () {
                    return fetch(key).catch(function (response) {
                        if (response && response.status === 403 && entries[key] === entry) {
                            delete entries[key];
                        }
                        return $q.reject(response);
                    });
                };
            }

            // Per participant, the ticket of the newest drink write applied to
            // them, so a refetch sent before that write does not undo it.
            entry.written = {};

            function keepNewerWrites(fetched, ticket) {
                var held = {};
                angular.forEach(entry.participants.data, function (participant) {
                    held[participant.id] = participant;
                });
                return fetched.map(function (participant) {
                    var newer = held[participant.id] && (entry.written[participant.id] || 0) > ticket;
                    return newer ? held[participant.id] : participant;
                });
            }

            entry.party = new Resource(forgetOnForbidden(RyyppyAPI.getParty));
            entry.participants = new Resource(forgetOnForbidden(RyyppyAPI.getPartyParticipants),
                {pollMs: POLL_MS, merge: keepNewerWrites});
            entry.invitations = new Resource(forgetOnForbidden(RyyppyAPI.getPartyInvitations));
            // null while the party has no join link.
            entry.joinLink = new Resource(forgetOnForbidden(function (partyId) {
                return RyyppyAPI.getJoinLink(partyId).catch(function (response) {
                    return response && response.status === 404 ? null : $q.reject(response);
                });
            }));
            return entry;
        }

        this.get = function (partyId) {
            var key = String(partyId);
            if (!entries[key]) {
                entries[key] = createEntry(key);
            }
            return entries[key];
        };

        this.forget = function (partyId) {
            delete entries[String(partyId)];
        };

        // The signed-in user is a participant of every party held here.
        this.invalidateAllParticipants = function (exceptPartyId) {
            angular.forEach(entries, function (entry, key) {
                if (key !== String(exceptPartyId)) {
                    entry.participants.invalidate();
                }
            });
        };

        // A participant as a drink write's response carried them; `ticket` is
        // the write's. Writes for different participants apply in any order.
        this.setParticipant = function (partyId, participant, ticket) {
            var entry = self.get(partyId);
            var participants = entry.participants;
            if (!participants.data) {
                participants.invalidate();
                return;
            }
            if (ticket < Math.max(participants.heldTicket(), entry.written[participant.id] || 0)) {
                return;
            }
            entry.written[participant.id] = ticket;
            // A new array, so components comparing by reference see the change.
            participants.set(participants.data.map(function (held) {
                return held.id === participant.id ? participant : held;
            }), participants.heldTicket());
        };

        function participantsChanged(partyId) {
            var entry = self.get(partyId);
            entry.participants.invalidate();
            entry.invitations.invalidate();
            // The party list shows each party's participants.
            PartiesStore.parties.invalidate();
        }

        this.addRegisteredUser = function (partyId, email) {
            return RyyppyAPI.addRegisteredUserToParty(partyId, email).then(function (data) {
                participantsChanged(partyId);
                return data;
            });
        };

        this.addGuest = function (partyId, guest) {
            return RyyppyAPI.addGuestToParty(partyId, guest).then(function (data) {
                participantsChanged(partyId);
                return data;
            });
        };

        this.invite = function (partyId, userId) {
            return RyyppyAPI.inviteUser(userId, partyId).then(function (data) {
                participantsChanged(partyId);
                return data;
            });
        };

        this.removeParticipant = function (partyId, participant) {
            return RyyppyAPI.removeParticipant(partyId, participant).then(function (data) {
                participantsChanged(partyId);
                return data;
            });
        };

        this.changeDuration = function (partyId, days) {
            var ticket = Resource.ticket();
            return RyyppyAPI.changePartyDuration(partyId, days).then(function (party) {
                self.get(partyId).party.set(party, ticket);
                PartiesStore.parties.invalidate();
                return party;
            });
        };

        this.createJoinLink = function (partyId) {
            var ticket = Resource.ticket();
            return RyyppyAPI.createJoinLink(partyId).then(function (joinLink) {
                self.get(partyId).joinLink.set(joinLink, ticket);
                return joinLink;
            });
        };

        this.revokeJoinLink = function (partyId) {
            var ticket = Resource.ticket();
            return RyyppyAPI.revokeJoinLink(partyId).then(function () {
                self.get(partyId).joinLink.set(null, ticket);
            });
        };

        this.leave = function (partyId, user) {
            return RyyppyAPI.removeParticipant(partyId, user).then(function (data) {
                delete entries[String(partyId)];
                PartiesStore.parties.invalidate();
                return data;
            });
        };
    }


    function ProfileStore(Resource, RyyppyAPI, InitialData, PartyStore) {
        var self = this;

        this.profile = new Resource(RyyppyAPI.getProfile, {pollMs: POLL_MS, initial: InitialData.get('PROFILE')});

        // Weight and sex feed into the promilles shown for the user everywhere.
        this.save = function (profile) {
            return RyyppyAPI.updateProfile(profile).then(function (data) {
                self.profile.invalidate();
                PartyStore.invalidateAllParticipants();
                return data;
            });
        };
    }


    function byTime(a, b) {
        return Date.parse(a.timestamp) - Date.parse(b.timestamp) || a.id - b.id;
    }

    /**
     * Every one of the signed-in user's own drinks, oldest first. The
     * dashboard lists the newest and graphs them all per day; writes change
     * the held array instead of refetching it.
     */
    function OwnDrinksStore(Resource, RyyppyAPI, InitialData) {
        var self = this;

        this.drinks = new Resource(RyyppyAPI.getOwnDrinks, {pollMs: POLL_MS, initial: InitialData.get('DRINKS')});

        // A new array each time, so components comparing by reference see the change.
        this.put = function (drink, ticket) {
            self.drinks.update(function (drinks) {
                return drinks.filter(function (held) {
                    return held.id !== drink.id;
                }).concat([drink]).sort(byTime);
            }, ticket);
        };

        this.remove = function (drinkId, ticket) {
            self.drinks.update(function (drinks) {
                return drinks.filter(function (held) {
                    return held.id !== drinkId;
                });
            }, ticket);
        };
    }


    /**
     * Adds, changes and removes drinks. Each write's response carries what
     * the page it was made on shows of the drinker, which goes straight into
     * the stores, and an own drink is applied to the own drinks held; what
     * other pages show of the drinker is refetched when next shown.
     */
    function Drinks($q, Resource, RyyppyAPI, ProfileStore, OwnDrinksStore, PartyStore) {
        function isOwn(drinker) {
            var profile = ProfileStore.profile.data;
            return drinker.type !== 'participant' || !profile || drinker.id === profile.id;
        }

        function applied(drinker, ticket, response) {
            if (isOwn(drinker) && response.drink) {
                OwnDrinksStore.put(response.drink, ticket);
            }
            if (drinker.type === 'participant') {
                PartyStore.setParticipant(drinker.partyId, response.participant, ticket);
                if (isOwn(drinker)) {
                    ProfileStore.profile.invalidate();
                    PartyStore.invalidateAllParticipants(drinker.partyId);
                }
            } else {
                ProfileStore.profile.set(response.profile, ticket);
                PartyStore.invalidateAllParticipants();
            }
        }

        function failed(drinker) {
            if (isOwn(drinker)) {
                ProfileStore.profile.invalidate();
                OwnDrinksStore.drinks.invalidate();
                PartyStore.invalidateAllParticipants();
            } else {
                PartyStore.get(drinker.partyId).participants.invalidate();
            }
        }

        // Resolves with the saved drink.
        this.add = function (drinker, drink) {
            var ticket = Resource.ticket();
            return RyyppyAPI.addDrink(drinker, drink).then(function (response) {
                applied(drinker, ticket, response);
                return response.drink;
            });
        };

        this.change = function (drinker, drinkId, drink) {
            var ticket = Resource.ticket();
            return RyyppyAPI.changeDrink(drinker, drinkId, drink).then(function (response) {
                applied(drinker, ticket, response);
                return response.drink;
            });
        };

        // The drink leaves the own drinks at once; a failed request refetches
        // it back.
        this.remove = function (drinker, drinkId) {
            var ticket = Resource.ticket();
            if (isOwn(drinker)) {
                OwnDrinksStore.remove(drinkId, ticket);
            }
            return RyyppyAPI.deleteDrink(drinker, drinkId).then(function (response) {
                applied(drinker, ticket, response);
            }, function (error) {
                failed(drinker);
                return $q.reject(error);
            });
        };
    }


    angular.module('ryyppy.services')
        .factory('Resource', ['$q', 'Poller', createResourceFactory])
        .service('PartiesStore', ['Resource', 'RyyppyAPI', 'InitialData', PartiesStore])
        .service('PartyStore', ['$q', 'Resource', 'RyyppyAPI', 'PartiesStore', PartyStore])
        .service('ProfileStore', ['Resource', 'RyyppyAPI', 'InitialData', 'PartyStore', ProfileStore])
        .service('OwnDrinksStore', ['Resource', 'RyyppyAPI', 'InitialData', OwnDrinksStore])
        .service('Drinks', ['$q', 'Resource', 'RyyppyAPI', 'ProfileStore', 'OwnDrinksStore', 'PartyStore', Drinks]);
})(angular);
