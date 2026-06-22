package sc.liste.noel.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GenericResponse implements Serializable {

    @JsonProperty("messageRetour")
    private String returnMessage;

    @JsonProperty("codeRetour")
    private int returnCode;

    public GenericResponse() {
    }

    public GenericResponse(String returnMessage, int returnCode) {
        this.returnMessage = returnMessage;
        this.returnCode = returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
