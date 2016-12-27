package com.melonlee.oauth2.service;

import com.melonlee.oauth2.entity.Client;

import java.util.List;

/**
 * Created by Melon on 16/12/22.
 */
public interface ClientService {

    public Client createClient(Client client);

    public Client updateClient(Client client);

    public void deleteClient(Long clientId);

    Client findOne(Long clientId);

    List<Client> findAll();

    Client findByClientId(String clientId);

    Client findByClientSecret(String clientSecret);

}
