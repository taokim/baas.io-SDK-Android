
package com.kth.baasio.callback;

import org.usergrid.java.client.response.ApiResponse;

public interface ApiResponseProgressCallback extends ClientProgressCallback<ApiResponse> {

    public void onResponse(ApiResponse response);

}
