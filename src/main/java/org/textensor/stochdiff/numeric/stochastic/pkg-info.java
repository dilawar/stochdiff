/**
 *
 *
 * These are the functions for the innner loops of the calculation.  Once the algorithms
 * are settled, they should probably be hand-converted to c for better performance.
 *
 * The main class is the StepGenerator that generates the number of particles that
 * make a particular transition given the total number, the probability of one particle
 * going, and a random number.
 *
 * There are two flavours - the DiscretePStepGenerator delegates to a set of
 * FixedPStepGenerators according to the value of p. This is applicable if the geometry,
 * reaction scheme and timestepping are controlled in such a way that there are
 * only a relativeley small number of different probability values in play (up to
 * a few hundred say).  The interpolating step generator works for continuous p,
 * but is rather slower (though there is plenty of scope for optimization).
 *
 * Each case ends up in a NGoTable which stores the cumulative probability and does the
 * lookup.
 *
 *
 *
 * The mixed stochastic/continuous calculation takes a ReactionTable for the reactions and a
 * VolumeGrid for the morphology and does the calculation. For high number densities the
 * update algorithm should use Dufort-Frankel. For lower densities, various exact and
 * approximate stochastic methods a la Blackwell.
 *
 *
 *
 *
 *   */
