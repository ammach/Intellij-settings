
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ${NAME}GatewayImpl implements ${NAME}Gateway {

    private final ${NAME}Client client;

    public ${NAME}GatewayImpl(${NAME}Client Client) {
        this.client = Client;
    }

    @Override
    public List<${NAME}Response> newMethod() {
        ResponseEntity<List<${NAME}Response>> response = client.methodName;
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new NotFoundException("cannot get intervention tree of project " + projectId + " for the organisation " + organisationId);
        }
        return response.getBody();
    }
}