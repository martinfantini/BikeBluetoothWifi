package com.example.bikebluetoothwifi.general;

public class DataCalculate {

        //roller radius in meters
        private final Double RollerRadio  = 0.05; // meters

        // 1 m/s = 3.6 km/h
        private final Double Conv_Cte_ms_kmh = 3.6;

        //This values actual distance in meters
        private Double Distance;
        //This values is in Km/h
        private Double Velocity;

        //Input: values is times the magnet pass by sensor
        public DataCalculate (Integer distanceData)
        {
                this.Distance = (2 * Math.PI * this.RollerRadio * distanceData );
        }

        //Input: time must be in Miliseconds
        //Output: the values is in km/h
        public String CalculateVelocity( double timer)
        {
            if(this.Distance < 0.00005)
                this.Velocity = 0.0;
            else
                this.Velocity = (this.Distance*1000*Conv_Cte_ms_kmh)/timer;
            return String.format("%.2f", this.Velocity);
        }

        //Output: Value in km/h
        public Double GetVelocity()
        {
            return this.Velocity;
        }

        //Output: Value in m
        public Double GetDistance()
        {
            return this.Distance;
        }
}
