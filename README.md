# o-tariff-comparison

O Tariff Comparison 

## Usage

The program can be ran with standard java:

    $ java o-tariff-comparison-0.1.0-SNAPSHOT-standalone.jar cost {power_usage} {gas_usage}
    $ java o-tariff-comparison-0.1.0-SNAPSHOT-standalone.jar usage {tariff_name} {fuel_type} {target_monthly_spend}

This is a lein project that can be run with the following command and params:

    $ lein run cost {power_usage} {gas_usage}
    $ lein run usage {tariff_name} {fuel_type} {target_monthly_spend}

## Testing

The test can be ran with lein:

    $ lein test

I am not sure how to run the tests with the uberjar..