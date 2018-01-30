package fe.api.gateway.listener;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by fe on 2018/1/29.
 */
@Builder
@Slf4j
public class CommonJsonRpcListener implements JsonRpcClient.RequestListener {
    @Override
    public void onBeforeRequestSent(JsonRpcClient client, ObjectNode request) {

    }

    @Override
    public void onBeforeResponseProcessed(JsonRpcClient client, ObjectNode response) {

    }
}
