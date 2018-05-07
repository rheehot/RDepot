/**
 * R Depot
 *
 * Copyright (C) 2012-2018 Open Analytics NV
 *
 * ===========================================================================
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * The Apache Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License for more details.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/>
 */
package eu.openanalytics.rdepot.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import eu.openanalytics.rdepot.authenticator.CustomBindAuthenticator;
import eu.openanalytics.rdepot.service.UserService;


@EnableGlobalMethodSecurity(prePostEnabled=true)
@ComponentScan("eu.openanalytics.rdepot")
public class SecurityConfig
{	
    
    @Resource
	private Environment env;
    
	private static final String PROPERTY_NAME_LDAP_URL = "ldap.url";
	private static final String PROPERTY_NAME_LDAP_BASEDN = "ldap.basedn";
	private static final String PROPERTY_NAME_LDAP_USEROU = "ldap.userou";
	private static final String PROPERTY_NAME_LDAP_LOGINFIELD = "ldap.loginfield";
	

	@Configuration
	@Order(1)
	public class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter 
	{
	    @Override
	    protected void configure(HttpSecurity http) throws Exception 
	    {
	        http
	        	.antMatcher("/api/**")
	        	.csrf().disable()
				.authorizeRequests()
					.anyRequest().hasAuthority("user")
					.and()
				.httpBasic()
				.and()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
	    }
	    
	    @Override
	    @Bean
	    public ProviderManager authenticationManager()
	    {
	    	return ldapAuthenticationManager();
	    }
	}
	
	@Configuration
	public class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter 
	{
		@Override
	    public void configure(WebSecurity web) throws Exception 
	    {
	        web
	        	.ignoring()
	            .antMatchers("/static/**");
	    }

	    @Override
	    protected void configure(HttpSecurity http) throws Exception 
	    {
	        http
	            .authorizeRequests()
	                .antMatchers("/manager**").hasAuthority("user")
	                .antMatchers("/static/**").permitAll()
	                //.anyRequest().hasAuthority("user")
	                .and()
	            .formLogin()
	                .loginPage("/login")
	                .defaultSuccessUrl("/manager")
	                .failureUrl("/loginfailed")
	                .permitAll().and()
	            .logout()
	            	//.logoutSuccessUrl("/login?success")
	            	.invalidateHttpSession(true);
	    }
	    
	    @Override
	    @Bean
	    public ProviderManager authenticationManager()
	    {
	    	return ldapAuthenticationManager();
	    }
	}
	
	@Bean
    public ProviderManager ldapAuthenticationManager()
    {
    	List<AuthenticationProvider> authenticationProviders = new ArrayList<AuthenticationProvider>();
    	
    	authenticationProviders.add(LDAPAuthenticationProvider());
    	
    	return new ProviderManager(authenticationProviders);
    }
    
    @Bean
    public LdapAuthoritiesPopulator ldapUserService() 
    {
        return new UserService();
    }
    
    @Bean
    public AbstractLdapAuthenticationProvider LDAPAuthenticationProvider() 
    {
    	LdapAuthenticationProvider ldapAuthProvider = new LdapAuthenticationProvider(LDAPAuthenticator(), ldapUserService());
    	return ldapAuthProvider;
    }
    
    @Bean
    public AbstractLdapAuthenticator LDAPAuthenticator() 
    {
    	CustomBindAuthenticator bind = new CustomBindAuthenticator(LDAPContextSource());
    	String userDnPattern = env.getRequiredProperty(PROPERTY_NAME_LDAP_LOGINFIELD) + "={0},ou=" + env.getRequiredProperty(PROPERTY_NAME_LDAP_USEROU);
    	bind.setUserDnPatterns(new String[]{userDnPattern});
    	bind.setUserSearch(userSearch());
    	return bind;
    }
    
    @Bean
    public DefaultSpringSecurityContextSource LDAPContextSource() 
    {
    	String url = env.getRequiredProperty(PROPERTY_NAME_LDAP_URL) + "/" + env.getRequiredProperty(PROPERTY_NAME_LDAP_BASEDN);
    	DefaultSpringSecurityContextSource ctxsrc = new DefaultSpringSecurityContextSource(url);
    	return ctxsrc;
    }
    
    @Bean
    public FilterBasedLdapUserSearch userSearch()
    {
    	String filter = "(" + env.getRequiredProperty(PROPERTY_NAME_LDAP_LOGINFIELD) + "={0})";
    	FilterBasedLdapUserSearch uSearch = new FilterBasedLdapUserSearch("", filter, LDAPContextSource());
    	return uSearch;
    }
}
