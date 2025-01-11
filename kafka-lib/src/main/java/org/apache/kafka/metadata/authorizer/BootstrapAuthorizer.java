package org.apache.kafka.metadata.authorizer;

import com.yammer.metrics.core.Counter;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;
import org.apache.kafka.server.metrics.KafkaMetricsGroup;
import org.apache.kafka.server.metrics.KafkaYammerMetrics;

import java.util.Collections;
import java.util.List;

public class BootstrapAuthorizer extends StandardAuthorizer {

    private final KafkaMetricsGroup metricsGroup = new KafkaMetricsGroup(BootstrapAuthorizer.class);
    private final Counter authCount = KafkaYammerMetrics.defaultRegistry().newCounter(metricsGroup.metricName("AuthCount", Collections.emptyMap()));

    @Override
    public List<AuthorizationResult> authorize(AuthorizableRequestContext requestContext, List<Action> actions) {
        authCount.inc();

        return List.of(AuthorizationResult.ALLOWED);
    }
}
