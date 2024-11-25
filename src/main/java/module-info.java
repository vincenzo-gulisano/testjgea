module flinkCEP.Patterns {
    requires flink.cep;
    requires io.github.ericmedvet.jgea.core;
    requires org.apache.commons.csv;
    requires io.github.ericmedvet.jgea.experimenter;
    requires java.logging;
    requires jcommander;
    requires io.github.ericmedvet.jnb.core;
    // requires flink.core;
    // requires flink.streaming.java;
    opens problem to io.github.ericmedvet.jnb.core;
}

