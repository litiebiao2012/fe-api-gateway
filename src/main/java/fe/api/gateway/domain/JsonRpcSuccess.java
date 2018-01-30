package fe.api.gateway.domain;

import lombok.Data;

/**
 * Created by fe on 2018/1/30.
 */
@Data
public class JsonRpcSuccess {
    private String jsonrpc;
    private long id;
    private JsonRpcSuccessDetail result;
}
