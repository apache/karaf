To use this module install jpa and hibernate features, then install bundles providing persistence units.

The hibernate module will provide one StatisticsMXBean for each persitence unit with an oname like this:
org.hibernate.statistics:unitName=<name of persistence unit>

Also see
https://docs.jboss.org/hibernate/core/4.2/javadocs/org/hibernate/stat/Statistics.html
