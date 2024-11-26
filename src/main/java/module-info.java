module flinkCEP.Patterns {
    // requires flink.cep;
    requires io.github.ericmedvet.jgea.core;
    requires io.github.ericmedvet.jgea.experimenter;
    requires io.github.ericmedvet.jnb.core;
    // requires flink.streaming.java;
    // requires flink.core;
    requires org.apache.commons.csv;
    requires flink.cep;
    requires merged.jar;
    opens problem to io.github.ericmedvet.jnb.core;
}

