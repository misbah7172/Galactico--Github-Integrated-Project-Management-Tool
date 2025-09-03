package com.autotrack.service;

import com.autotrack.model.User;
import com.autotrack.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for user management and OAuth2 authentication.
 */
@Service
public class UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all users.
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by ID.
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    /**
     * Get user by GitHub ID.
     */
    public User getUserByGitHubId(String gitHubId) {
        return userRepository.findByGitHubId(gitHubId)
                .orElseThrow(() -> new RuntimeException("User not found with GitHub ID: " + gitHubId));
    }

    /**
     * Get user by email.
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Get user by nickname.
     */
    public User getUserByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("User not found with nickname: " + nickname));
    }

    /**
     * Process OAuth2 login and create/update user.
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        
        // Extract user attributes
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String gitHubId = attributes.get("id").toString();
        String nickname = attributes.get("login").toString();
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");
        
        // Find or create user
        User user = userRepository.findByGitHubId(gitHubId)
                .orElseGet(() -> {
                    // Check if a user already exists with this nickname
                    return userRepository.findByNickname(nickname)
                            .map(existingUser -> {
                                // Update existing user with GitHub ID and other info
                                existingUser.setGitHubId(gitHubId);
                                if (email != null && !email.equals(existingUser.getEmail())) {
                                    existingUser.setEmail(email);
                                }
                                if (avatarUrl != null && !avatarUrl.equals(existingUser.getAvatarUrl())) {
                                    existingUser.setAvatarUrl(avatarUrl);
                                }
                                existingUser.setUpdatedAt(java.time.LocalDateTime.now());
                                return userRepository.save(existingUser);
                            })
                            .orElseGet(() -> createUser(gitHubId, nickname, email, avatarUrl));
                });
        
        // For existing users found by GitHub ID, update information if it has changed
        if (user.getGitHubId().equals(gitHubId)) {
            boolean needsUpdate = false;
            if (!nickname.equals(user.getNickname())) {
                user.setNickname(nickname);
                needsUpdate = true;
            }
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                needsUpdate = true;
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                needsUpdate = true;
            }
            
            if (needsUpdate) {
                user.setUpdatedAt(java.time.LocalDateTime.now());
                userRepository.save(user);
            }
        }
        
        // Create authorities from user roles
        Collection<GrantedAuthority> authorities;
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            authorities = user.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .collect(Collectors.toList());
        } else {
            // Default authority for users without specific roles
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }
        
        // Return OAuth2User with user authorities
        return new DefaultOAuth2User(
                authorities,
                attributes,
                "login"
        );
    }

    /**
     * Create a new user.
     */
    @Transactional
    public User createUser(String gitHubId, String nickname, String email, String avatarUrl) {
        User user = new User();
        user.setGitHubId(gitHubId);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setAvatarUrl(avatarUrl);
        
        // Initialize roles as a mutable list
        List<Role> roles = new ArrayList<>();
        roles.add(Role.MEMBER);
        user.setRoles(roles);
        
        // Set timestamps
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Update a user's role.
     */
    @Transactional
    public User updateUserRole(Long userId, List<Role> roles) {
        User user = getUserById(userId);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    /**
     * Get current user from OAuth2User.
     */
    public User getCurrentUser(OAuth2User principal) {
        Object gitHubIdObj = principal.getAttribute("id");
        Object nicknameObj = principal.getAttribute("login");
        
        if (gitHubIdObj == null || nicknameObj == null) {
            throw new RuntimeException("GitHub ID or login is missing from OAuth2 attributes");
        }
        
        String gitHubId = gitHubIdObj.toString();
        String nickname = nicknameObj.toString();
        String email = (String) principal.getAttribute("email");
        String avatarUrl = (String) principal.getAttribute("avatar_url");
        
        // Find or create user if not exists
        return userRepository.findByGitHubId(gitHubId)
                .orElseGet(() -> createUser(gitHubId, nickname, email, avatarUrl));
    }

    /**
     * Find or create user by GitHub username for team invitations.
     */
    @Transactional
    public User findOrCreateUserByGitHub(String githubUsername, String commitUsername, String email, boolean isTeamLead) {
        User existingUser = userRepository.findByNickname(commitUsername).orElse(null);
        if (existingUser != null) {
            return existingUser;
        }
        
        User user = new User();
        user.setGitHubId("pending_" + githubUsername);
        user.setNickname(commitUsername);
        user.setEmail(email);
        user.setAvatarUrl("https://github.com/" + githubUsername + ".png");
        
        List<Role> roles = new ArrayList<>();
        if (isTeamLead) {
            roles.add(Role.TEAM_LEAD);
        } else {
            roles.add(Role.MEMBER);
        }
        user.setRoles(roles);
        
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        return userRepository.save(user);
    }

    /**
     * Update user's nickname for commit tracking.
     */
    @Transactional
    public void updateUserNickname(Long userId, String newNickname) {
        User user = getUserById(userId);
        user.setNickname(newNickname);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Update user's GitHub username for commit tracking.
     */
    @Transactional
    public User updateGithubUsername(Long userId, String githubUsername) {
        User user = getUserById(userId);
        
        // Check if the GitHub username is already taken by another user
        userRepository.findByGitHubId(githubUsername)
            .filter(existingUser -> !existingUser.getId().equals(userId))
            .ifPresent(existingUser -> {
                throw new RuntimeException("GitHub username '" + githubUsername + "' is already taken by another user");
            });
        
        user.setGitHubId(githubUsername);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        
        return userRepository.save(user);
    }
}
