package io.jonashackt.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jonashackt.businesslogic.IncredibleLogic;
import io.jonashackt.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
public class WeatherBackendController {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherBackendController.class);

    private ObjectMapper objectMapper = new ObjectMapper();;

    @RequestMapping(path = "/general/outlook", method=RequestMethod.POST, produces="application/json")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody GeneralOutlook generateGeneralOutlook(@RequestBody Weather weather) throws JsonProcessingException {
        LOG.info("Request for /general/outlook with POST");

        // Some incredible Businesslogic...
        LOG.info("Called Backend");

        LOG.info("Object as JSON:" + objectMapper.writeValueAsString(weather));

        GeneralOutlook outlook = IncredibleLogic.generateGeneralOutlook();
        LOG.info("GeneralOutlook as JSON: \n " + objectMapper.writeValueAsString(outlook));

        return outlook;
    }

    @RequestMapping(path = "/general/outlook", method=RequestMethod.GET, produces="application/json")
    @ResponseStatus(HttpStatus.OK)
    public @ResponseBody String infoAboutGeneralOutlook() throws JsonProcessingException {
        LOG.info("Request for /general/outlook with GET");

        Weather weather = new Weather();
        weather.setFlagColor("blue");
        weather.setPostalCode("99425");
        weather.addUser(new User(55, 5634500, MethodOfPayment.Bitcoin));
        weather.setProduct(Product.ForecastBasic);

        String weatherJson = objectMapper.writeValueAsString(weather);

        return "Try a POST also against this URL! Just send some body with it like: '" + weatherJson + "'";
    }

    @RequestMapping(value = "/{name}", method = RequestMethod.GET, produces = "text/plain")
    public String whatsTheSenseInThat(@PathVariable("name") String name) {
        LOG.info("Request for /{name} with GET");
        return "Hello " + name + "! This is a RESTful HttpService written in Spring. Try to use some other HTTP verbs (don´t say 'methods' :P ) :)";
    }
}
