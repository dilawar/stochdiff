/**
 *
 * Representation of the morphology and discretized mesh for computational purposes.
 *
 * When a morphology is first read in, objects are instantiated from the main morphology
 * class which is orientated towards input and output. These objects sort themselves out
 * (dereference themselves) and generate an array of TreePoints which is an equivalent
 * representation of the structure more suited to doing operations on.
 *
 * Then the discretization methods take this structure and generate a VolumeGrid according to the
 * discretization criteria. The VolumeGrid should be pretty simple - just volume elements and
 * lists of neighbors for each one along with coupling constants.
 *
 */
