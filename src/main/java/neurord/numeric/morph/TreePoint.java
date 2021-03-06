package neurord.numeric.morph;

import neurord.geom.Position;

import java.util.ArrayList;
import java.util.HashMap;

public class TreePoint implements Position {

    private double x;
    private double y;
    private double z;
    private double r;

    public TreePoint[] nbr;
    public int nnbr;
    public TreePoint parent;

    // this is set for a point which is at the same place as its
    // parent but starts a new segment with a different radius
    public TreePoint subAreaPeer = null;

    public String label;

    // temporary work variables
    public int iwork;
    public boolean dead; // marking prior to removal


    // x and y positions when part of a dendrogram, set by MLC
    public double dgx;
    public double dgy;

    private ArrayList<TreePoint> offsetChildren;

    private HashMap<TreePoint, String> segidHM;
    private HashMap<TreePoint, String> regionHM;
    private String firstRegion = null;

    public String name; // not sure about this POSERR;

    // work field used while setting up child branches
    public double partBranchOffset = 0.;


    public TreePoint() {
        this.nbr = new TreePoint[6];
        this.nnbr = 0;
        this.dead = false;
    }

    public TreePoint(double x, double y, double z, double r) {
        this();
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
    }

    public double getRadius() {
        return r;
    }

    public void setRadius(double r) {
        this.r = r;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getLabel() {
        return label;
    }

    public TreePoint getParent() {
        return parent;
    }

    public TreePoint makeCopy() {
        return new TreePoint(x, y, z, r);
    }

    public void setWork(int iw) {
        iwork = iw;
    }

    public int getWork() {
        return iwork;
    }

    public String toString() {
        return ("(point x=" + x + ",y=" + y + ",z=" + z + ",r=" + r + ",nnbr=" + nnbr + ")");
    }

    public void locateBetween(TreePoint cpa, TreePoint cpb, double f) {
        double wf = 1. - f;
        x = f * cpb.x + wf * cpa.x;
        y = f * cpb.y + wf * cpa.y;
        z = f * cpb.z + wf * cpa.z;
        r = f * cpb.r + wf * cpa.r;

    }

    // REFAC - these should all be private, so only the
    // static methods that preserve symmetry are visible
    public void addNeighbor(TreePoint cpn) {
        for (int i = 0; i < nnbr; i++)
            if (nbr[i] == cpn)
                throw new RuntimeException("adding a neighbor we already have ");

        if (nnbr >= nbr.length) {
            TreePoint[] pn = new TreePoint[2 * nnbr];
            for (int i = 0; i < nnbr; i++) {
                pn[i] = nbr[i];
            }
            nbr = pn;
        }
        nbr[nnbr++] = cpn;
    }

    public void removeNeighbor(TreePoint cp) {
        int ii = -1;
        for (int i = 0; i < nnbr; i++)
            if (nbr[i] == cp)
                ii = i;
        if (ii >= 0) {
            for (int i = ii; i < nnbr - 1; i++)
                nbr[i] = nbr[i + 1];
            nnbr--;
        }
    }

    public void replaceNeighbor(TreePoint cp, TreePoint cr) {
        int ii = -1;
        for (int i = 0; i < nnbr; i++)
            if (nbr[i] == cp)
                ii = i;

        if (ii >= 0)
            nbr[ii] = cr;
        else
            throw new RuntimeException("(replaceNeighbor) couldn't find nbr " + cp + " in nbrs list of " + this);

        if (segidHM != null && segidHM.containsKey(cp))
            segidHM.put(cr, segidHM.get(cp));

        if (regionHM != null && regionHM.containsKey(cp))
            regionHM.put(cr, regionHM.get(cp));
    }

    public boolean hasNeighbor(TreePoint cp) {
        for (int i = 0; i < nnbr; i++)
            if (nbr[i] == cp)
                return true;

        return false;
    }

    public void removeDeadNeighbors() {
        for (int i = nnbr - 1; i >= 0; i--)
            if (nbr[i].dead)
                removeNeighbor(nbr[i]);
    }

    // these are branches that start some way down a segment, but are
    // linked from here temporarily until the tree is discretized and a new point
    // is available to have them connected from as neighbors
    public void addOffsetChild(TreePoint p) {
        if (offsetChildren == null)
            offsetChildren = new ArrayList<>();

        offsetChildren.add(p);
    }

    public boolean hasOffsetChildren() {
        return offsetChildren != null;
    }

    public ArrayList<TreePoint> getOffsetChildren() {
        return offsetChildren;
    }

    public double distanceTo(TreePoint cp) {
        double dx = x - cp.x;
        double dy = y - cp.y;
        double dz = z - cp.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void movePerp(TreePoint ca, TreePoint cb, double dperp) {
        double dx = cb.x - ca.x;
        double dy = cb.y - ca.y;
        double f = Math.sqrt(dx * dx + dy * dy);
        dx /= f;
        dy /= f;
        x += dperp * dy;
        y -= dperp * dx;
    }

    public static void neighborize(TreePoint tp, TreePoint tpn) {
        tp.addNeighbor(tpn);
        tpn.addNeighbor(tp);

    }

    public ArrayList<TreePoint> getNeighbors() {
        ArrayList<TreePoint> ret = new ArrayList<>();
        for (int i = 0; i < nnbr; i++) {
            ret.add(nbr[i]);
        }
        return ret;
    }


    public boolean isEndPoint() {
        return nnbr == 1;
    }

    public TreePoint oppositeNeighbor(TreePoint tpp) {
        if (nnbr != 2)
            return null;
        if (tpp == nbr[0])
            return nbr[1];
        else
            return nbr[0];
    }

    public void setLabel(String s) {
        label = s;
    }

    public synchronized void setIDWith(TreePoint point, String s) {
        if (this.segidHM == null)
            this.segidHM = new HashMap<>();

        //  E.info("tp set region id to " + point + " as " + s);
        this.segidHM.put(point, s);
    }

    public synchronized void setRegionWith(TreePoint point, String s) {
        if (this.regionHM == null) {
            this.regionHM = new HashMap<>();
            this.firstRegion = s;
        }
        this.regionHM.put(point, s);
    }


    public String regionClassWith(TreePoint tp) {
        String ret = null;
        if (this.regionHM != null)
            ret = this.regionHM.get(tp);

        // June 2009: this could change a lot of things: previously
        // we required both ends to be in the same region. Now it is
        // enough to have the first point of a slice as long as there isn't
        // a potential ambiguity (eg this point in two regions, but its
        // neighbor in neither of them)
        if (ret == null)
            ret = soleRegion();

        return ret;
    }

    public String soleRegion() {
        if (this.regionHM != null && this.regionHM.size() == 1)
            return this.firstRegion;

        return null;
    }

    public String segmentIDWith(TreePoint tp) {
        if (this.segidHM != null)
            return this.segidHM.get(tp);

        return null;
    }

    public void setSubAreaOf(TreePoint cpa) {
        subAreaPeer = cpa;
    }

    public void alignTop(TreePoint tpon, TreePoint tpdir, double offset) {
        // align the "top" of this point ofset below the top of tpon
        // in the direction perpendicular to the line to tpdir
        // in genearl the structure will be:
        // tpir ---- tpon - this     where "this" is the beginning of
        // a smaller branch at the same position as tpon but with smaller
        // radius

        // E.info("top alignment " + tpdir + " " + tpon + " " + this + " offset=" +offset);

        double dx = tpon.getX() - tpdir.getX();
        double dy = tpon.getY() - tpdir.getY();
        double l = Math.sqrt(dx * dx + dy * dy);
        double vx = -dy / l;
        double vy = dx / l;

        double dist = offset - (tpon.getRadius() - getRadius());

        x = tpon.getX() + dist * vx;
        y = tpon.getY() + dist * vy;
        z = tpon.getZ();
    }

    public TreePoint largestNeighborNot(TreePoint cpb) {
        TreePoint ret = null;
        for (int i = 0; i < nnbr; i++)
            if ((ret == null && nbr[i] != cpb) ||
                    (ret != null && nbr[i].r > ret.r))
                ret = nbr[i];

        return ret;
    }
}
