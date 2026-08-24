package tools.vitruv.methodologisttemplate.consistency.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import mir.reactions.model2Model2.Model2Model2ChangePropagationSpecification;
import org.junit.jupiter.api.Test;
import tools.vitruv.dsls.reactions.runtime.reactions.AbstractReactionsChangePropagationSpecification;
import tools.vitruv.dsls.reactions.runtime.reactions.Reaction;

/**
 * Guards against {@link HookedModel2Model2ChangePropagationSpecification#setup()} drifting from
 * the generated {@link Model2Model2ChangePropagationSpecification#setup()} it must mirror (see
 * that class's Javadoc for why it can't just delegate to {@code super.setup()}).
 */
class HookedModel2Model2ChangePropagationSpecificationTest {

  @Test
  void wrapsExactlyTheSameReactionsAsTheGeneratedSpecification() throws Exception {
    var plain = new Model2Model2ChangePropagationSpecification();
    var hooked = new HookedModel2Model2ChangePropagationSpecification();

    Set<Class<?>> plainReactionClasses = reactionClassesOf(plain);
    Set<Class<?>> hookedReactionClasses = unwrappedReactionClassesOf(hooked);

    assertEquals(
        plainReactionClasses,
        hookedReactionClasses,
        "HookedModel2Model2ChangePropagationSpecification.setup() has drifted from the generated "
            + "Model2Model2ChangePropagationSpecification.setup() -- update both when a Reaction "
            + "is added, renamed, or removed in templateReactions.reactions");
  }

  @SuppressWarnings("unchecked")
  private static List<Reaction> reactionsFieldOf(Object specification) throws Exception {
    Field field =
        AbstractReactionsChangePropagationSpecification.class.getDeclaredField("reactions");
    field.setAccessible(true);
    return (List<Reaction>) field.get(specification);
  }

  private static Set<Class<?>> reactionClassesOf(Object specification) throws Exception {
    return reactionsFieldOf(specification).stream()
        .map(Object::getClass)
        .collect(Collectors.toSet());
  }

  private static Set<Class<?>> unwrappedReactionClassesOf(Object specification) throws Exception {
    return reactionsFieldOf(specification).stream()
        .map(r -> (Class<?>) ((HookedReaction) r).wrappedReactionClass())
        .collect(Collectors.toSet());
  }
}
