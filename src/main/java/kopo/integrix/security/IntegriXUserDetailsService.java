package kopo.integrix.security;

import kopo.integrix.entity.UserInfoEntity;
import kopo.integrix.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IntegriXUserDetailsService implements UserDetailsService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final UserInfoRepository userInfoRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserInfoEntity user = userInfoRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String role = user.getRole() == null || user.getRole().isBlank() ? "USER" : user.getRole();

        return User.withUsername(user.getUserId())
                .password(user.getPassword())
                .authorities("ROLE_" + role)
                .disabled(!STATUS_ACTIVE.equals(user.getStatus()))
                .build();
    }
}
