package com.example.service.Impl;

import com.example.dto.request.AuthRequestDTO;
import com.example.dto.response.AuthResponseDTO;
import com.example.entity.Profile;
import com.example.entity.User;
import com.example.exception.BadRequestException;
import com.example.exception.OAuth2AuthenticationProcessingException;
import com.example.model.UserPrincipal;
import com.example.security.manager.JJwtManager;
import com.example.security.manager.PasswordManager;
import com.example.security.oauth2.userinfo.OAuth2UserInfo;
import com.example.security.oauth2.userinfo.OAuth2UserInfoFactory;
import com.example.service.IAuthService;
import com.example.service.IProfileService;
import com.example.service.IRoleService;
import com.example.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends DefaultOAuth2UserService
        implements IAuthService {

    private final IUserService userService;
    private final IRoleService roleService;
    private final IProfileService profileService;
    private final JJwtManager manager;
    private final PasswordManager passwordManager;

    public Object setSecurityContextBefore(Authentication authentication) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        return authentication.getPrincipal();
    }

    @Override
    public AuthResponseDTO login(AuthRequestDTO requestDTO) {
        return new AuthResponseDTO(manager
                .createToken((UserPrincipal) setSecurityContextBefore(
                        authenticate(requestDTO.getEmail(), requestDTO.getPassword()))));
    }

    @Override
    public AuthResponseDTO register(AuthRequestDTO requestDTO) {
        if(userService.existsByEmail(requestDTO.getEmail()))
            throw new BadRequestException("Email already registered");
        userService.register(new User(requestDTO.getEmail(),
                    passwordManager.encodePassword(requestDTO.getPassword()),
                    true, true, true,
                true, false, false,
                roleService.getUserRoles()));
        return login(requestDTO);
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        return new UserPrincipal(userService.findByEmail(username), null);
    }

    public UserDetails checkUserDetails(UserDetails userDetails,
                                        String password) {
        if(userDetails == null)
            throw new BadRequestException("Invalid Username or Password");
        if(!passwordManager.matches(password, userDetails.getPassword()))
            throw new BadRequestException("Invalid Password");
        return userDetails;
    }

    public Authentication authenticate(String username, String password) {
        return new UsernamePasswordAuthenticationToken(
                checkUserDetails(loadUserByUsername(username), password), null);
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            return processOAuth2User(userRequest, super.loadUser(userRequest));
        } catch (Exception e) {
            throw new InternalAuthenticationServiceException(
                    "The email does not match any account");
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest request,
                                         OAuth2User user) {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(request
                .getClientRegistration().getRegistrationId(), user.getAttributes());
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank())
            throw new OAuth2AuthenticationProcessingException(
                    "Email not found from Oauth2 Provider");
        if(!userService.existsByEmail(userInfo.getEmail())) {
            return new UserPrincipal(registerProfile(
                    userService.register(new User(userInfo.getEmail(),
                            passwordManager.encodePassword(UUID.randomUUID().toString()),
                            true, true, true,
                            true, true, true,
                            roleService.getUserRoles())), userInfo), user.getAttributes());
        }
        return new UserPrincipal(userService.findByEmail(userInfo.getEmail()),
                user.getAttributes());
    }

    private User registerProfile(User user, OAuth2UserInfo userInfo) {
        profileService.save(new Profile(userInfo.getFirstName(),
                userInfo.getLastName(), "", userInfo.getImageUrl(), user));
        return user;
    }
}