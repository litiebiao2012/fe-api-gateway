package fe.api.gateway.domain;

import fe.api.gateway.common.JsonRpcClient;
import lombok.Data;

/**
 * Created by fe on 2018/1/30.
 */
@Data
public class JsonError {
    private String jsonrpc;
    private long id;
    private JsonErrorDetail error;
}
