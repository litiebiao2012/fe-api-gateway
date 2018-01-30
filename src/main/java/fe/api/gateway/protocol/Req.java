package fe.api.gateway.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by fe on 2018/1/26.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Req {
    /**
     * 唯一id
     */
    private long id;
    /**
     * method方法名
     * magellan.product.findByUserName
     */
    private String method;
    /**
     * 业务数据
     */
    private String params;
    /**
     * 版本
     */
    private String version;

}
