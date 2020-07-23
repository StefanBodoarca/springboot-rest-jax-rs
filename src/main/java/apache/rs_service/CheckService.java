package apache.rs_service;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/check-service")
public class CheckService {
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String checkService() {
        return "Service running!";
    }
}