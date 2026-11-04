package com.buffspotions.flagon;

/** A placed flagon a dispenser can fill or empty. */
public interface FlagonVessel {
    FlagonContents contents();

    void setContents(FlagonContents contents);
}
