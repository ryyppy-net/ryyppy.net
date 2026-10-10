package drinkcounter;

import drinkcounter.model.Friend;
import drinkcounter.model.User;
import drinkcounter.repository.UserRepository;
import drinkcounter.web.controllers.api.v2.GravatarUrls;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationSuggestions {

    private final UserRepository userRepository;
    private final EntityManager em;

    public InvitationSuggestions(UserRepository userRepository, EntityManager em) {
        this.userRepository = userRepository;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public List<Friend> forParty(int userId, int partyId, int amount) {
        TypedQuery<Object[]> q = em.createQuery("select distinct par.id, p.startTime \n" +
                " from Party p join p.participants par \n" +
                " where par.id not in(select pp.id from Party p join p.participants pp where p.id = :partyId)\n" +
                " and p in (select party from Party party join party.participants participant where participant.id = :userId)\n" +
                " and par.guest = false\n" +
                " order by p.startTime desc", Object[].class);
        q.setParameter("partyId", partyId);
        q.setParameter("userId", userId);
        q.setFirstResult(0);
        q.setMaxResults(amount);
        List<Object[]> results = q.getResultList();
        List<Friend> friends = new ArrayList<Friend>();
        for (Object[] tuple : results) {
            User user = userRepository.findById((Integer)tuple[0]).orElseThrow(EntityNotFoundException::new);
            friends.add(new Friend(user.getId(), user.getName(), GravatarUrls.forUser(user)));
        }
        return friends;
    }
}
