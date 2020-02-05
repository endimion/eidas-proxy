/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.aegean.eidasproxy.utils;

import eu.eidas.sp.SpAuthenticationResponseData;
import eu.eidas.sp.SpEidasSamlTools;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nikos
 */
public class EidasUtils {

    private final static Logger LOG = LoggerFactory.getLogger(EidasUtils.class);

    private final static Pattern namePattern = Pattern.compile("friendlyName='(.*?)'");
    private final static Pattern valuePattern = Pattern.compile("=\\[(.*?)\\]");

    public static Map<String, String> parseEidasResponse(String saml, String remoteHost) {
        Map<String, String> map = new HashMap();
        try {
            SpAuthenticationResponseData data = SpEidasSamlTools.processResponse(saml, remoteHost);
            Optional<String> error = checkEidasResponseForErrorCode(data.getResponseXML());
            if (error.isPresent()) {
                map.put("error", error.get());
                return map;
            }

            return parse(data.getResponseXML());
        } catch (Exception e) {
            LOG.error(e.getMessage());

        }
        return map;
    }

    public static Map<String, String> parse(String eIDASResponse) throws IndexOutOfBoundsException {

//        LOG.info("got the eIDAS response:" + eIDASResponse);
        Map<String, String> result = new HashMap();
        String attributePart = eIDASResponse.split("attributes='")[1];
        String[] attributesStrings = attributePart.split("AttributeDefinition");
        Arrays.stream(attributesStrings).filter(string -> {
            return string.indexOf("=") > 0;
        }).filter(string -> {
            return namePattern.matcher(string).find();
        }).forEach(attrString -> {
            Matcher nameMatcher = namePattern.matcher(attrString);
            Matcher valueMatcher = valuePattern.matcher(attrString);
            if (valueMatcher.find() && nameMatcher.find()) {
                String name = nameMatcher.group(1);
                char c[] = name.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                name = new String(c);
                String value = valueMatcher.group(1);
                result.put(name, value);
                if (name.equals("personIdentifier")) {
                    result.put("eid", value);
                }
            }
        });

        String loaPart = eIDASResponse.split("levelOfAssurance='")[1].split("',")[0];
        result.put("loa", loaPart);

        return result;
    }

    public static Optional<String> checkEidasResponseForErrorCode(String eidasResponse) {
        if (eidasResponse.contains("202007") || eidasResponse.contains("202004")
                || eidasResponse.contains("202012") || eidasResponse.contains("202010")
                || eidasResponse.contains("003002")) {
            if (eidasResponse.contains("202007") || eidasResponse.contains("202012")) {
                LOG.debug("---------------202012!!!!!!!");
                return Optional.of("Cancelled");
            }
            if (eidasResponse.contains("202004")) {
                return Optional.of("Cancelled");
            }
            if (eidasResponse.contains("202010")) {
                return Optional.of("Cancelled");
            }
            if (eidasResponse.contains("003002")) {
                return Optional.of("ERROR");
            }
        }
        return Optional.empty();
    }

}
