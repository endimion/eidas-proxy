/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.aegean.eidasproxy.service;

import java.io.IOException;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import org.springframework.stereotype.Service;

/**
 *
 * @author nikos
 */
@Service
public class MemcachedService {

    MemcachedClient mcc;

    public MemcachedService() throws IOException {
        this.mcc = new MemcachedClient(AddrUtil.getAddresses("memcached:11211"));
    }

    public MemcachedClient getCache() {
        return this.mcc;
    }
}
