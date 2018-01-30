package fe.api.gateway.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by fe on 2018/1/29.
 */
@Builder
@Data
@AllArgsConstructor
public class EndPoint {
    private String ip;
    private int port;
    private String serviceInterface;
    private String method;
    private String params;
}
