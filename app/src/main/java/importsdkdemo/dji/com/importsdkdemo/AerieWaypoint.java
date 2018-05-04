package importsdkdemo.dji.com.importsdkdemo;

public class AerieWaypoint {
    public String user_id;
    public String sector_id;
    public String waypoint_id;
    public double lng;
    public double lat;
    public float  alt;
    public AerieWaypoint(String user_id, String sector_id, String waypoint_id, double lng, double lat, double alt) {
        this.user_id = user_id;
        this.sector_id = sector_id;
        this.waypoint_id = waypoint_id;
        this.lng = lng;
        this.lat = lat;
        this.alt = (float)alt;
    }
}
