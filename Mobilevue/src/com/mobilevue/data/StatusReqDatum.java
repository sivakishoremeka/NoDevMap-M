package com.mobilevue.data;

import com.google.gson.annotations.Expose;


public class StatusReqDatum {

@Expose
private String status;

public String getStatus() {
return status;
}

public void setStatus(String status) {
this.status = status;
}

}