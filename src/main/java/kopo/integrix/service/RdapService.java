package kopo.integrix.service;

import java.util.Optional;

public interface RdapService {

    Optional<RdapDomainInfo> lookupDomain(String host);

    record RdapDomainInfo(
            String domain,
            String registrar,
            String registrationDate,
            String expirationDate,
            Long domainAgeDays
    ) {
    }
}
