package fe.api.gateway.domain;

import lombok.Data;

/**
 * Created by fe on 2018/1/30.
 */
@Data
public class JsonErrorDetail {
    private int code;
    private String message;
    private Object data;
}
