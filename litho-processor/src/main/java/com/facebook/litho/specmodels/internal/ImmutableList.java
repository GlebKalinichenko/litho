/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.specmodels.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A dummy representation of an immutable set. This can be used temporarily as a type until we have
 * an actual non-guava implementation.
 */
public class ImmutableList<E> extends ArrayList<E> {

  private ImmutableList(final int capacity) {
    super(capacity);
  }

  private ImmutableList(List<E> list) {
    super(list);
  }

  public static <E> ImmutableList<E> copyOf(List<E> list) {
    return new ImmutableList<>(list);
  }

  public static <E> ImmutableList<E> of(E... elements) {
    final ImmutableList<E> list = new ImmutableList<>(elements.length);
    Collections.addAll(list, elements);
    return list;
  }
}
