/*
 * Copyright 2018 datagear.tech. All Rights Reserved.
 */

package org.datagear.model.features;

import org.datagear.model.PropertyFeature;
import org.datagear.model.ValueFeature;

/**
 * 最大值。
 * 
 * @author datagear@163.com
 *
 */
public class Max extends ValueFeature<Number> implements PropertyFeature
{
	public Max()
	{
		super();
	}

	public Max(Number value)
	{
		super(value);
	}
}
