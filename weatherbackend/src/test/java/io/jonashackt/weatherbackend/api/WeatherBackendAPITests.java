package io.jonashackt.weatherbackend.api;

import io.jonashackt.weatherbackend.WeatherBackendApplication;
import io.jonashackt.weatherbackend.businesslogic.IncredibleLogic;
import io.jonashackt.weatherbackend.model.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = WeatherBackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class WeatherBackendAPITests {

    @LocalServerPort
    int port;

    @Before
    public void init() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

	@Test
    public void should_create_weather() {
	    
	    Weather weather = new Weather();
	    weather.setFlagColor("blue");
	    weather.setPostalCode("99425");
	    weather.setProduct(Product.ForecastBasic);
        weather.addUser(new User(27, 4300, MethodOfPayment.Bitcoin));
        weather.addUser(new User(45, 500300, MethodOfPayment.Paypal));
        weather.addUser(new User(67, 60000300, MethodOfPayment.Paypal));

	    given() // can be ommited when GET only
	        .contentType(ContentType.JSON)
            .body(weather)
        .when() // can be ommited when GET only
            .post("/weather/general/outlook")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .contentType(ContentType.JSON)
            .assertThat()
                .equals(IncredibleLogic.generateGeneralOutlook());
	    
	    GeneralOutlook outlook = given() // can be ommited when GET only
	            .contentType(ContentType.JSON)
	            .body(weather).post("/weather/general/outlook").as(GeneralOutlook.class);
	    
	    assertEquals("Weimar", outlook.getCity());
    }

}
