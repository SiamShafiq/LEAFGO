package com.example.leaf;

import java.util.List;

public class PlaceDetails {

    public List<Result> result;

    public class Result{
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
}
