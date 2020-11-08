package com.example.leaf;

import java.util.List;

public class PlaceDetails {

    public List<Result> results;

    public class Result{
        transient boolean visited;

        String place_id;
        String name;
        double rating;
        int price_rating;

        public Geometry geometry;
        public class Geometry{

            public Location location;
            public class Location{
                double lat;
                double lng;
            }
        }
    }

    public boolean setVisited(String placeid){
        for(Result result: this.results) {
            if(result.place_id.equals(placeid)) {
                result.visited = true;
                return true;
            }
        }
        return false;
    }
}
