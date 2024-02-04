package graphql.agent;

import graphql.agent.result.ExecutionTrackingResult;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;

import static graphql.agent.result.ExecutionTrackingResult.EXECUTION_TRACKING_KEY;
import static net.bytebuddy.matcher.ElementMatchers.nameMatches;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class GraphQLJavaAgent {

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent is running");
        new AgentBuilder.Default()
//                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
//                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .disableClassFormatChanges()
                .type(named("graphql.execution.ExecutionStrategy"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    System.out.println("Transforming " + typeDescription);
                    return builder
                            .visit(Advice.to(DataFetcherInvokeAdvice.class).on(nameMatches("invokeDataFetcher")));
                })
                .installOn(inst);

        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .type(named("graphql.execution.Execution"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    System.out.println("transforming " + typeDescription);
                    return builder
                            .visit(Advice.to(ExecutionAdvice.class).on(nameMatches("executeOperation")));
                }).installOn(inst);
    }
}

class ExecutionAdvice {

    @Advice.OnMethodEnter
    public static void executeOperationEnter(@Advice.Argument(0) ExecutionContext executionContext) {
        executionContext.getGraphQLContext().put(EXECUTION_TRACKING_KEY, new ExecutionTrackingResult());
    }
}

class DataFetcherInvokeAdvice {
    @Advice.OnMethodEnter
    public static void invokeDataFetcherEnter(@Advice.Argument(0) ExecutionContext executionContext,
                                              @Advice.Argument(1) ExecutionStrategyParameters parameters) {
        ExecutionTrackingResult executionTrackingResult = executionContext.getGraphQLContext().get(EXECUTION_TRACKING_KEY);
        executionTrackingResult.start(parameters.getPath(), System.nanoTime());
    }

    @Advice.OnMethodExit
    public static void invokeDataFetcherExit(@Advice.Argument(0) ExecutionContext executionContext,
                                             @Advice.Argument(1) ExecutionStrategyParameters parameters) {
        ExecutionTrackingResult executionTrackingResult = executionContext.getGraphQLContext().get(EXECUTION_TRACKING_KEY);
        executionTrackingResult.end(parameters.getPath(), System.nanoTime());
    }

}
