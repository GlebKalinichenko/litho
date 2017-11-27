/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentsPools.acquireMountContent;
import static com.facebook.litho.ComponentsPools.canAddMountContentToPool;
import static com.facebook.litho.ComponentsPools.release;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.app.Activity;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ActivityController;

@RunWith(ComponentsTestRunner.class)
public class ComponentsPoolsTest {
  private final ComponentLifecycle mLifecycle =
      new ComponentLifecycle() {
        @Override
        int getTypeId() {
          return 1;
        }

        @Override
        public View onCreateMountContent(ComponentContext context) {
          return new View(context);
        }
      };

  private ComponentContext mContext1;
  private ComponentContext mContext2;
  private ComponentContext mContext3;
  private ActivityController<Activity> mActivityController;
  private Activity mActivity;
  private ComponentContext mActivityComponentContext;
  private ColorDrawable mMountContent;

  @Before
  public void setup() {
    mContext1 = new ComponentContext(RuntimeEnvironment.application);
    mContext2 = new ComponentContext(new ComponentContext(RuntimeEnvironment.application));
    mContext3 = new ComponentContext(new ContextWrapper(RuntimeEnvironment.application));
    mActivityController = Robolectric.buildActivity(Activity.class).create();
    mActivity = mActivityController.get();
    mActivityComponentContext = new ComponentContext(mActivity);
    mMountContent = new ColorDrawable(Color.RED);
  }

  @After
  public void tearDown() {
    ComponentsPools.clearActivityCallbacks();
  }

  @Test
  public void testAcquireMountContentWithSameContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle.getTypeId())).isNull();

    release(mContext1, mLifecycle, mMountContent);

    assertThat(mMountContent).isSameAs(acquireMountContent(mContext1, mLifecycle.getTypeId()));
  }

  @Test
  public void testAcquireMountContentWithSameUnderlyingContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle.getTypeId())).isNull();

    release(mContext1, mLifecycle, mMountContent);

    assertThat(mMountContent).isSameAs(acquireMountContent(mContext2, mLifecycle.getTypeId()));
  }

  @Test
  public void testAcquireMountContentWithDifferentUnderlyingContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle.getTypeId())).isNull();

    release(mContext1, mLifecycle, mMountContent);

    assertThat(acquireMountContent(mContext3, mLifecycle.getTypeId())).isNull();
  }

  @Test
  public void testReleaseMountContentForDestroyedContextDoesNothing() {
    // Assert pooling was working before
    assertThat(acquireMountContent(mActivityComponentContext, mLifecycle.getTypeId())).isNull();

    release(mActivityComponentContext, mLifecycle, mMountContent);

    assertThat(mMountContent)
        .isSameAs(acquireMountContent(mActivityComponentContext, mLifecycle.getTypeId()));

    // Now destroy it and assert pooling no longer works
    mActivityController.destroy();
    release(mActivityComponentContext, mLifecycle, mMountContent);

    assertThat(acquireMountContent(mActivityComponentContext, mLifecycle.getTypeId())).isNull();
  }

  @Test
  public void testDestroyingActivityDoesNotAffectPoolingOfOtherContexts() {
    mActivityController.destroy();
    ComponentsPools.onContextDestroyed(mActivity);

    release(mContext1, mLifecycle, mMountContent);

    assertThat(acquireMountContent(mContext1, mLifecycle.getTypeId())).isSameAs(mMountContent);
  }

  @Test
  public void testCanAddMountContentToPool() {
    // Necessary because this is the only way to allocate a Pool for the first time
    ComponentsPools.acquireMountContent(mContext1, mLifecycle.getTypeId());

    assertThat(canAddMountContentToPool(RuntimeEnvironment.application, mLifecycle)).isTrue();
    assertThat(canAddMountContentToPool(mContext1, mLifecycle)).isTrue();
    assertThat(canAddMountContentToPool(mContext2, mLifecycle)).isTrue();
    assertThat(canAddMountContentToPool(mContext3, mLifecycle)).isTrue();

    for (int i = 0; i < mLifecycle.poolSize(); i++) {
      mLifecycle.preAllocateMountContent(mContext1);
    }

    assertThat(canAddMountContentToPool(RuntimeEnvironment.application, mLifecycle)).isFalse();
    assertThat(canAddMountContentToPool(mContext1, mLifecycle)).isFalse();
    assertThat(canAddMountContentToPool(mContext2, mLifecycle)).isFalse();
    // This has a different underlying Context, so it should not be preallocated for this Context
    assertThat(canAddMountContentToPool(mContext3, mLifecycle)).isTrue();
  }
}
