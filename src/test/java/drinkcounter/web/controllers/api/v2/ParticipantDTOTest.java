package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.User;
import org.junit.Assert;
import org.junit.Test;

public class ParticipantDTOTest {
    @Test
    public void gravatarUrlIsFormedProperly() throws Exception {
        User user = new User();
        user.setId(1);
        user.setSex(User.Sex.MALE);
        user.setWeight(66);
        user.setEmail("matti.meikalainen@example.org");
        ParticipantDTO participant = ParticipantDTO.fromUser(user);
        Assert.assertEquals("http://www.gravatar.com/avatar/56f4f87e829c34b149d35e0e1a2ff08d.jpg?d=wavatar", participant.getProfilePictureUrl());
    }
}
