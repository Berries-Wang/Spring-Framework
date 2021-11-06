package link.bosswang.domain;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class NameReq implements Serializable {
    private static final long serialVersionUID = -1L;

    @NotBlank(message = "name不允许为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
