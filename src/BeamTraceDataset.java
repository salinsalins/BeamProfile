
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.PublicCloneable;

/**
 *
 * @author sanin
 */
public class BeamTraceDataset extends AbstractXYDataset
        implements XYDataset, PublicCloneable {
    
    private List<Series> seriesList;

    /**
     * Creates a new <code>BeamTraceDataset</code> instance,
     * initially containing no data.
     */
    public BeamTraceDataset() {
        this.seriesList = new java.util.ArrayList<>();
    }

    /**
     * Returns the number of series in the dataset.
     *
     * @return The series count.
     */
    @Override
    public int getSeriesCount() {
        return this.seriesList.size();
    }

    /**
     * Returns the key for a series.
     *
     * @param index the series index (in the range <code>0</code> to
     *     <code>getSeriesCount() - 1</code>).
     *
     * @return The key for the series.
     *
     * @throws IndexOutOfBoundsException if <code>series</code> is not in the
     *     specified range.
     */
    @Override
    public Comparable getSeriesKey(int index) {
        if ((index < 0) || (index >= getSeriesCount())) {
            throw new IndexOutOfBoundsException("Series index out of bounds");
        }
        return index;
    }

    int indexOf(Series series) {
        return this.seriesList.indexOf(series);
    }

    /**
     * Returns the order of the domain (x-) values in the dataset.  In this
     * implementation, we cannot guarantee that the x-values are ordered, so
     * this method returns <code>DomainOrder.NONE</code>.
     *
     * @return <code>DomainOrder.NONE</code>.
     */
    @Override
    public DomainOrder getDomainOrder() {
        return DomainOrder.NONE;
    }

    /**
     * Returns the number of items in the specified series.
     *
     * @param series the series index (in the range <code>0</code> to
     *     <code>getSeriesCount() - 1</code>).
     *
     * @return The item count.
     *
     * @throws IllegalArgumentException if <code>series</code> is not in the
     *     specified range.
     */
    @Override
    public int getItemCount(int series) {
        if ((series < 0) || (series >= getSeriesCount())) {
            throw new IllegalArgumentException("Series index out of bounds");
        }
        return seriesList.get(series).x.length;
    }

    /**
     * Returns the x-value for an item within a series.
     *
     * @param series  the series index (in the range <code>0</code> to
     *     <code>getSeriesCount() - 1</code>).
     * @param item  the item index (in the range <code>0</code> to
     *     <code>getItemCount(series)</code>).
     *
     * @return The x-value.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not
     *     within the specified range.
     * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not
     *     within the specified range.
     *
     * @see #getX(int, int)
     */
    @Override
    public double getXValue(int series, int item) {
        return seriesList.get(series).x[item];
    }

    /**
     * Returns the x-value for an item within a series.
     *
     * @param series  the series index (in the range <code>0</code> to
     *     <code>getSeriesCount() - 1</code>).
     * @param item  the item index (in the range <code>0</code> to
     *     <code>getItemCount(series)</code>).
     *
     * @return The x-value.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not
     *     within the specified range.
     * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not
     *     within the specified range.
     *
     * @see #getXValue(int, int)
     */
    @Override
    public Number getX(int series, int item) {
        return new Double(getXValue(series, item));
    }

    /**
     * Returns the y-value for an item within a series.
     *
     * @param series  the series index (in the range <code>0</code> to
     *     <code>getSeriesCount() - 1</code>).
     * @param item  the item index (in the range <code>0</code> to
     *     <code>getItemCount(series)</code>).
     *
     * @return The y-value.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not
     *     within the specified range.
     * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not
     *     within the specified range.
     *
     * @see #getY(int, int)
     */
    @Override
    public double getYValue(int series, int item) {
        return seriesList.get(series).y[item];
    }

    /**
     * Returns the y-value for an item within a series.
     *
     * @param series  the series index (in the range <code>0</code> to
     *     <code>getSeriesCount() - 1</code>).
     * @param item  the item index (in the range <code>0</code> to
     *     <code>getItemCount(series)</code>).
     *
     * @return The y-value.
     *
     * @throws ArrayIndexOutOfBoundsException if <code>series</code> is not
     *     within the specified range.
     * @throws ArrayIndexOutOfBoundsException if <code>item</code> is not
     *     within the specified range.
     *
     * @see #getX(int, int)
     */
    @Override
    public Number getY(int series, int item) {
        return new Double(getYValue(series, item));
    }

    /**
     * Adds a series, then sends a {@link DatasetChangeEvent} to
     * all registered listeners.
     *
     * @param x
     * @param y
     */
    public void addSeries(double[] x, double[] y) {
        this.seriesList.add(new Series(x, y));
        notifyListeners(new DatasetChangeEvent(this, this));
    }

    /**
     * Removes a series from the dataset, then sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     *
     * @param index  the series index (<code>null</code> not permitted).
     *
     */
    public void removeSeries(int index) {
        this.seriesList.remove(index);
        notifyListeners(new DatasetChangeEvent(this, this));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeamTraceDataset)) {
            return false;
        }
        BeamTraceDataset that = (BeamTraceDataset) obj;
        if (this.seriesList.size() != that.seriesList.size()) {
            return false;
        }
        for (int i = 0; i < this.seriesList.size(); i++) {
            Series s1 = this.seriesList.get(i);
            Series s2 = that.seriesList.get(i);
            if (s1 != s2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code int.
     */
    @Override
    public int hashCode() {
        return 29 + this.seriesList.hashCode();
    }

    /**
     * Creates an independent copy of this dataset.
     *
     * @return The cloned dataset.
     *
     * @throws CloneNotSupportedException if there is a problem cloning the
     *     dataset (for instance, if a non-cloneable object is used for a
     *     series key).
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        BeamTraceDataset clone = new BeamTraceDataset();
        clone.seriesList = new ArrayList(this.seriesList.size());
        for (int i = 0; i < this.seriesList.size(); i++) {
            clone.seriesList.add(this.seriesList.get(i));
        }
        return clone;
    }
    
    private class Series {
        double[] x;
        double[] y;
        
        Series(double[] _x, double[] _y) throws IllegalArgumentException {
            if (x == null || y == null)
                throw new IllegalArgumentException("X and Y cannot be null");
            if (_x.length != _y.length)
                throw new IllegalArgumentException("X and Y size mismatch");
            x = _x;
            y = _y;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + Arrays.hashCode(this.x);
            hash = 67 * hash + Arrays.hashCode(this.y);
            return hash;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Series)) {
                return false;
            }
            if (obj == this) {
            return true;
            }
            return false;
        }
    }

}