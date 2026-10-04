-- Sessions signed in with an OAuth2 principal resolve to no user, since the app
-- only resolves DrinkcounterUserDetails. Their owners sign in again.
delete from spring_session
where primary_id in (
    select session_primary_id
    from spring_session_attributes
    where attribute_name = 'SPRING_SECURITY_CONTEXT'
      and encode(attribute_bytes, 'hex') like '%' || encode('org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken'::bytea, 'hex') || '%'
);
