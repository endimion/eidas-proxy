/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.aegean.eidasproxy.controllers;

import eu.eidas.sp.SpAuthenticationRequestData;
import eu.eidas.sp.SpAuthenticationResponseData;
import eu.eidas.sp.SpEidasSamlTools;
import eu.eidas.sp.metadata.GenerateMetadataAction;
import gr.aegean.eidasproxy.pojo.EidasResponse;
import gr.aegean.eidasproxy.service.MemcachedService;
import gr.aegean.eidasproxy.utils.EidasUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author nikos
 */
@Controller
@CrossOrigin
public class RestController {

    private final static String SP_COUNTRY = "GR";
    private final static Logger LOG = LoggerFactory.getLogger(RestController.class);

    @Autowired
    MemcachedService mcs;

    @GetMapping("/testRequest")
    public @ResponseBody
    String getSamlRequest() {

        ArrayList<String> pal = new ArrayList();
        pal.add("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName");
        pal.add("http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName");
        pal.add("http://eidas.europa.eu/attributes/naturalperson/DateOfBirth");
        pal.add("http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier");
        SpAuthenticationRequestData data
                = SpEidasSamlTools.generateEIDASRequest(pal, "GR", "GR");
        return data.getSaml();
    }

    @GetMapping("/makeRequest")
    public @ResponseBody
    String makeParamSamlRequest(@RequestParam String country, @RequestParam String keycloakSession) {

        ArrayList<String> pal = new ArrayList();
        pal.add("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName");
        pal.add("http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName");
        pal.add("http://eidas.europa.eu/attributes/naturalperson/DateOfBirth");
        pal.add("http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier");
        SpAuthenticationRequestData data
                = SpEidasSamlTools.generateEIDASRequest(pal, country, SP_COUNTRY);

        // the generated eIDAS session
        String eidasSession = data.getID();
        LOG.info("will cache: " + eidasSession + " with " + keycloakSession);
        mcs.getCache().add(eidasSession, 300, keycloakSession);
        return data.getSaml();
    }

    @PostMapping("/processResponse")
    public @ResponseBody
    Map<String, String> porcessSamlResponse(@RequestBody EidasResponse response) {
        if (response.getSamlResponse().equals("test")) {
            Map<String, String> fake = new HashMap<>();
            fake.put("PersonIdentifier", "123");
            fake.put("CurrentGivenName", "nikos");
            fake.put("CurrentFamilyName", "triantafyllou");
            fake.put("DateOfBirth", "12/12/12");
            return fake;
        }
        SpAuthenticationResponseData data = SpEidasSamlTools.processResponse(response.getSamlResponse(), response.getRemoteAddress());
        String eidasSession = data.getResponseToID();//data.getID();
        LOG.info("got eIDAS session: " + eidasSession);
        String keycloakSession = (String) this.mcs.getCache().get(eidasSession);
        LOG.info("found keycloak session: " + keycloakSession);

        Map<String, String> result = EidasUtils.parseEidasResponse(response.getSamlResponse(), response.getRemoteAddress());
        result.put("keycloakSession", keycloakSession);

        return result;

    }

    @RequestMapping(value = "/metadata", method = {RequestMethod.POST, RequestMethod.GET}, produces = {"application/xml"})
    public @ResponseBody
    String metadata() {
        GenerateMetadataAction metaData = new GenerateMetadataAction();
        return metaData.generateMetadata().trim();
    }

    @RequestMapping(value = "/authenticationSuccess", method = {RequestMethod.POST, RequestMethod.GET}, produces = {"application/xml"})
    public @ResponseBody
    String testAuthSuccess() {

        return "Success!!";
    }

}
