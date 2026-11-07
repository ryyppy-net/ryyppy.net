package drinkcounter.authentication.authorizationserver;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import drinkcounter.authentication.DrinkcounterUserDetails;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * The JSON form of a stored authorization's attributes, which hold the signed-in user's
 * {@link DrinkcounterUserDetails}. Only the email and user id are written, never the password hash.
 */
public final class AuthorizationJson {

    private AuthorizationJson() {
    }

    public static JsonMapper mapper() {
        List<JacksonModule> modules = new ArrayList<>(SecurityJacksonModules.getModules(AuthorizationJson.class.getClassLoader()));
        modules.add(new SimpleModule("DrinkcounterUserDetails")
                .setMixInAnnotation(DrinkcounterUserDetails.class, DrinkcounterUserDetailsMixin.class));
        return JsonMapper.builder().addModules(modules).build();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = DrinkcounterUserDetailsDeserializer.class)
    @JsonAutoDetect(fieldVisibility = Visibility.NONE, getterVisibility = Visibility.NONE,
            isGetterVisibility = Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class DrinkcounterUserDetailsMixin {

        @JsonProperty("username")
        abstract String getUsername();

        @JsonProperty("userId")
        abstract int getUserId();
    }

    static class DrinkcounterUserDetailsDeserializer extends ValueDeserializer<DrinkcounterUserDetails> {

        @Override
        public DrinkcounterUserDetails deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
            JsonNode node = context.readTree(parser);
            DrinkcounterUserDetails details = new DrinkcounterUserDetails(
                    node.get("username").asString(), "", true, true, true, true,
                    Set.of(new SimpleGrantedAuthority("ROLE_USER")), node.get("userId").asInt());
            details.eraseCredentials();
            return details;
        }
    }
}
