/*global angular */

(function (angular) {
    'use strict';

    var POLL_MS = 60000;

    /**
     * One piece of server data shared by every component that shows it.
     * Components read `data` and keep it current with use(); writes call
     * invalidate(), which refetches now if the data is shown and on the next
     * use() otherwise.
     */
    function createResourceFactory($q, Poller) {
        return function Resource(fetch, options) {
            options = options || {};
            var self = this;
            var pollMs = options.pollMs || 0;
            var fetchedAt = null;
            var stale = false;
            var latest = 0;
            var pending = null;
            var users = 0;
            var stopPolling = null;

            this.data = null;

            if (options.initial !== null && options.initial !== undefined) {
                self.data = options.initial;
                fetchedAt = Date.now();
            }

            // Only the latest request's response is kept, so a poll that was
            // already in flight when the data changed cannot overwrite it.
            this.refresh = function () {
                var request = ++latest;
                pending = fetch().then(function (data) {
                    if (request === latest) {
                        self.data = data;
                        fetchedAt = Date.now();
                        stale = false;
                    }
                    return self.data;
                }).finally(function () {
                    if (request === latest) {
                        pending = null;
                    }
                });
                return pending;
            };

            function isCurrent() {
                return fetchedAt !== null && !stale && !(pollMs && Date.now() - fetchedAt >= pollMs);
            }

            // Resolves with the data, fetching it only when what is held is
            // missing, invalidated, or older than a poll interval.
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
                self.load();
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
        };
    }


    function PartiesStore(Resource, RyyppyAPI, InitialData) {
        var self = this;

        this.parties = new Resource(RyyppyAPI.getParties, {initial: InitialData.get('PARTIES')});

        this.addParty = function (name) {
            return RyyppyAPI.addParty(name).then(function (party) {
                self.parties.invalidate();
                return party;
            });
        };
    }


    /**
     * A party, its participants and the users that can be invited to it, per
     * party id. Only party members may read these (docs/access-rules.md), so
     * a party's entry is dropped once the user leaves or is removed from it.
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

            entry.party = new Resource(forgetOnForbidden(RyyppyAPI.getParty));
            entry.participants = new Resource(forgetOnForbidden(RyyppyAPI.getPartyParticipants), {pollMs: POLL_MS});
            entry.invitations = new Resource(forgetOnForbidden(RyyppyAPI.getPartyInvitations));
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
        this.invalidateAllParticipants = function () {
            angular.forEach(entries, function (entry) {
                entry.participants.invalidate();
            });
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


    /** The signed-in user's own drinks and per-day drink history. */
    function OwnDrinksStore(Resource, RyyppyAPI, InitialData) {
        this.drinks = new Resource(RyyppyAPI.getOwnDrinks, {pollMs: POLL_MS, initial: InitialData.get('DRINKS')});
        this.history = new Resource(RyyppyAPI.getDrinkHistory, {initial: InitialData.get('DRINK_HISTORY')});
    }


    /**
     * Adds, changes and removes drinks, refreshing every store the drink
     * shows up in.
     */
    function Drinks(RyyppyAPI, ProfileStore, OwnDrinksStore, PartyStore) {
        function isOwn(drinker) {
            var profile = ProfileStore.profile.data;
            return drinker.type !== 'participant' || !profile || drinker.id === profile.id;
        }

        function changed(drinker) {
            if (isOwn(drinker)) {
                ProfileStore.profile.invalidate();
                OwnDrinksStore.drinks.invalidate();
                OwnDrinksStore.history.invalidate();
                PartyStore.invalidateAllParticipants();
            } else {
                PartyStore.get(drinker.partyId).participants.invalidate();
            }
        }

        this.add = function (drinker, drink) {
            return RyyppyAPI.addDrink(drinker, drink).then(function (saved) {
                changed(drinker);
                return saved;
            });
        };

        this.change = function (drinker, drinkId, drink) {
            return RyyppyAPI.changeDrink(drinker, drinkId, drink).then(function (data) {
                changed(drinker);
                return data;
            });
        };

        this.remove = function (drinker, drinkId) {
            return RyyppyAPI.deleteDrink(drinker, drinkId).then(function (data) {
                changed(drinker);
                return data;
            });
        };
    }


    angular.module('ryyppy.services')
        .factory('Resource', ['$q', 'Poller', createResourceFactory])
        .service('PartiesStore', ['Resource', 'RyyppyAPI', 'InitialData', PartiesStore])
        .service('PartyStore', ['$q', 'Resource', 'RyyppyAPI', 'PartiesStore', PartyStore])
        .service('ProfileStore', ['Resource', 'RyyppyAPI', 'InitialData', 'PartyStore', ProfileStore])
        .service('OwnDrinksStore', ['Resource', 'RyyppyAPI', 'InitialData', OwnDrinksStore])
        .service('Drinks', ['RyyppyAPI', 'ProfileStore', 'OwnDrinksStore', 'PartyStore', Drinks]);
})(angular);
