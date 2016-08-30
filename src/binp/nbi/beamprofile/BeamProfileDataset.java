 /*
 * ---------------------
 * Based on DefaultXYDataset.java
 * from jFreeChart library
 * ---------------------
 * (C) Copyright 2016 by All.
 *
 * Author:  Andrey Sanin;
 *
 * Changes
 * -------
 * 29-Aug-2016 : Creation;
 *
 */
package binp.nbi.beamprofile;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jfree.data.DomainOrder;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.util.PublicCloneable;

/**
 *
 * @author Sanin
 */

public class BeamProfileDataset extends AbstractXYDataset
        implements XYDataset, PublicCloneable {
    
    /**
     * Storage for the series in the dataset.  We use a list because the
     * order of the series is significant.  This list must be kept in sync
     * with the seriesKeys list.
     */
    private List<Integer> seriesList;
    
    // Data array
    double[][] data;

    /**
     * Creates a new <code>BeamProfileDataset</code> instance, initially
     * containing no data.
     * @param d - data array
     */
    public BeamProfileDataset(double[][] d) {
        data = d;
        this.seriesList = new java.util.ArrayList();
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
     * @param series  the series index (in the range <code>0</code> to
     *     <code>getSeriesCount() - 1</code>).
     *
     * @return The key for the series.
     *
     * @throws IllegalArgumentException if <code>series</code> is not in the
     *     specified range.
     */
    @Override
    public Comparable getSeriesKey(int series) {
        if ((series < 0) || (series >= getSeriesCount())) {
            throw new IllegalArgumentException("Series index out of bounds");
        }
        return (Comparable) this.seriesList.get(series);
    }

    /**
     * Returns the index of the series with the specified key, or -1 if there
     * is no such series in the dataset.
     *
     * @param seriesKey  the series key (<code>null</code> permitted).
     *
     * @return The index, or -1.
     */
    @Override
    public int indexOf(Comparable seriesKey) {
        return this.seriesList.indexOf(seriesKey);
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
     * @param series  the series index (in the range <code>0</code> to
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
        return data.length;
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
        return data[item][0];
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
        int index = seriesList.get(series);
        return data[item][index];
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
     * Adds a series or if a series with the same key already exists replaces
     * the data for that series, then sends a {@link DatasetChangeEvent} to
     * all registered listeners.
     *
     * @param seriesKey  the series key (<code>null</code> not permitted).
     * @param data  the data (must be an array with length 2, containing two
     *     arrays of equal length, the first containing the x-values and the
     *     second containing the y-values).
     */
    public void addSeries(Integer seriesKey) {
        if (seriesKey == null) {
            throw new IllegalArgumentException(
                    "The 'seriesKey' cannot be null.");
        }
        int seriesIndex = indexOf(seriesKey);
        if (seriesIndex == -1) {  // add a new series
            this.seriesList.add(seriesKey);
        }
        else {  // replace an existing series
            this.seriesList.remove(seriesIndex);
            this.seriesList.add(seriesIndex, seriesKey);
        }
        notifyListeners(new DatasetChangeEvent(this, this));
    }

    /**
     * Removes a series from the dataset, then sends a
     * {@link DatasetChangeEvent} to all registered listeners.
     *
     * @param seriesKey  the series key (<code>null</code> not permitted).
     *
     */
    public void removeSeries(Integer seriesKey) {
        int seriesIndex = indexOf(seriesKey);
        if (seriesIndex >= 0) {
           this.seriesList.remove(seriesIndex);
            notifyListeners(new DatasetChangeEvent(this, this));
        }
    }

    /**
     * Tests this <code>DefaultXYDataset</code> instance for equality with an
     * arbitrary object.  This method returns <code>true</code> if and only if:
     * <ul>
     * <li><code>obj</code> is not <code>null</code>;</li>
     * <li><code>obj</code> is an instance of
     *         <code>DefaultXYDataset</code>;</li>
     * <li>both datasets have the same number of series, each containing
     *         exactly the same values.</li>
     * </ul>
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeamProfileDataset)) {
            return false;
        }
        BeamProfileDataset that = (BeamProfileDataset) obj;
        if (this.seriesList.size() != that.seriesList.size()) {
            return false;
        }
        if (this.data != that.data) {
            return false;
        }
        for (int i = 0; i < this.seriesList.size(); i++) {
            int d1 = this.seriesList.get(i);
            int d2 = that.seriesList.get(i);
            if (d1 != d2) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code.
     */
    @Override
    public int hashCode() {
        int result;
        result = Arrays.hashCode(data);
        result = 29 * result + this.seriesList.hashCode();
        return result;
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
        BeamProfileDataset clone = (BeamProfileDataset) super.clone();
        clone.seriesList = new ArrayList(this.seriesList.size());
        for (int i = 0; i < this.seriesList.size(); i++) {
            clone.seriesList.add(this.seriesList.get(i));
        }
        clone.data = data;
        return clone;
    }

}
