package com.mobilevue.retrofit;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

import com.mobilevue.data.ActivePlanDatum;
import com.mobilevue.data.ClientDatum;
import com.mobilevue.data.ClientnConfigDatum;
import com.mobilevue.data.EPGData;
import com.mobilevue.data.MediaDetailRes;
import com.mobilevue.data.MediaDetailsResDatum;
import com.mobilevue.data.OrderDatum;
import com.mobilevue.data.PlanDatum;
import com.mobilevue.data.ResourceIdentifier;
import com.mobilevue.data.ServiceDatum;
import com.mobilevue.data.StatusReqDatum;
import com.mobilevue.data.TemplateDatum;

public interface OBSClient {
	
	@GET("/mediadevices/client/{clientId}")
	ClientnConfigDatum getClientnConfigDataSync(
			@Path("clientId") String clientId);

	@GET("/orders/{clientId}/activeplans")
	void getActivePlans(@Path("clientId") String clientId,
			Callback<List<ActivePlanDatum>> cb);

	@GET("/clients/template")
	void getTemplate(Callback<TemplateDatum> cb);

	@GET("/plans?planType=prepaid")
	void getPrepaidPlans(Callback<List<PlanDatum>> cb);

	@GET("/planservices/{clientId}?serviceType=IPTV")
	ArrayList<ServiceDatum> getPlanServicesSync(
			@Path("clientId") String clientId);

	@GET("/planservices/{clientId}?serviceType=IPTV")
	void getPlanServices(@Path("clientId") String clientId,
			Callback<List<ServiceDatum>> cb);

	@GET("/epgprogramguide/{channelName}/{reqDate}")
	void getEPGDetails(@Path("channelName") String channelName,
			@Path("reqDate") String reqDate, Callback<EPGData> cb);

	@GET("/assets")
	void getPageCountAndMediaDetails(@Query("filterType") String category,
			@Query("pageNo") String pageNo, @Query("deviceId") String deviceId,
			Callback<MediaDetailRes> cb);

	@GET("/assetdetails/{mediaId}")
	void getMediaDetails(@Path("mediaId") String mediaId,
			@Query("eventId") String eventId,
			@Query("deviceId") String deviceId,
			Callback<MediaDetailsResDatum> cb);

	@GET("/clients/{clientId}")
	ClientDatum getClinetDetailsSync(@Path("clientId") String clientId);
	
	@GET("/clients/{clientId}")
	void getClinetDetails(@Path("clientId") String clientId,
			Callback<ClientDatum> cb);

	@GET("/orders/{clientId}/orders")
	void getClinetPackageDetails(@Path("clientId") String clientId,
			Callback<List<OrderDatum>> cb);
	
	@PUT("/selfcare/status/{clientId}")
	ResourceIdentifier updateAppStatus(@Path("clientId") String clientId,@Body StatusReqDatum request);
	
}
