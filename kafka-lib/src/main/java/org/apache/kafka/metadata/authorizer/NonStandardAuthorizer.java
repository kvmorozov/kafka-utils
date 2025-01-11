package org.apache.kafka.metadata.authorizer;

import com.yammer.metrics.core.Counter;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.requests.RequestContext;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;
import org.apache.kafka.server.metrics.KafkaMetricsGroup;
import org.apache.kafka.server.metrics.KafkaYammerMetrics;

import java.util.Collections;
import java.util.List;

import static org.apache.kafka.common.resource.PatternType.LITERAL;
import static org.apache.kafka.common.resource.ResourceType.TOPIC;

public class NonStandardAuthorizer extends StandardAuthorizer {

    private static String PRINCIPAL_TYPE_BROKER = "Broker";
    private static String PRINCIPAL_TYPE_CLIENT = "Client";

    private final KafkaMetricsGroup metricsGroup = new KafkaMetricsGroup(NonStandardAuthorizer.class);
    private final Counter authCount = KafkaYammerMetrics.defaultRegistry().newCounter(metricsGroup.metricName("AuthCount", Collections.emptyMap()));

    private final ResourcePattern DEFAULT_RESOURCE_PATTERN = new ResourcePattern(TOPIC, "name", LITERAL);

    @Override
    public List<AuthorizationResult> authorize(AuthorizableRequestContext requestContext, List<Action> actions) {
        authCount.inc();

        if (actions.isEmpty())
            if (requestContext instanceof RequestContext rc) {
                return super.authorize(rebuildRqWithPricipalType(rc, PRINCIPAL_TYPE_CLIENT), List.of(new Action(AclOperation.DESCRIBE,
                        DEFAULT_RESOURCE_PATTERN, 0, true, true)));
            } else
                return List.of(defaultResult());
        else {
            if (requestContext instanceof RequestContext rc) {
                String principalType = switch (actions.get(0).operation()) {
                    case CLUSTER_ACTION, DESCRIBE_CONFIGS, IDEMPOTENT_WRITE -> PRINCIPAL_TYPE_BROKER;
                    default -> PRINCIPAL_TYPE_CLIENT;
                };

                return super.authorize(rebuildRqWithPricipalType(rc, principalType), actions);
            } else
                return super.authorize(requestContext, actions);
        }
    }

    private RequestContext rebuildRqWithPricipalType(RequestContext rc, String principalType) {
        return new RequestContext(rc.header, rc.connectionId, rc.clientAddress,
                new KafkaPrincipal(principalType, rc.header.clientId()),
                rc.listenerName, rc.securityProtocol, rc.clientInformation, false);
    }
}
