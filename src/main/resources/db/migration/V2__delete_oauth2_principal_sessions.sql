-- Signs out sessions whose principal is an OAuth2User; the app expects DrinkcounterUserDetails.
-- Session attributes are Java-serialized, so the token's class name appears in the bytes.
DELETE FROM spring_session
WHERE primary_id IN (
    SELECT session_primary_id
    FROM spring_session_attributes
    WHERE attribute_name = 'SPRING_SECURITY_CONTEXT'
      AND encode(attribute_bytes, 'hex') LIKE
          '%' || encode('org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken'::bytea, 'hex') || '%'
);
