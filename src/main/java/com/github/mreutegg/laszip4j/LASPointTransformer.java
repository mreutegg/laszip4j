/*
 * Copyright 2023 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j;

/**
 * Allows an implementation to transform a point.
 */
public interface LASPointTransformer {

    /**
     * This is a noop transformation that does not modify the point.
     */
    LASPointTransformer NONE = (point, modifier) -> {};

    /**
     * Potentially transform the given point using the modifier.
     *
     * @param point    the current point.
     * @param modifier the modifier to apply transformations to the current point.
     */
    void transform(LASPoint point, LASPointModifier modifier);
}
